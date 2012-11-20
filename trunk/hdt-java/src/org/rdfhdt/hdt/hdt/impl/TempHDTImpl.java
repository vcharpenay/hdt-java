/**
 * File: $HeadURL$
 * Revision: $Rev$
 * Last modified: $Date$
 * Last modified by: $Author$
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * Contacting the authors:
 *   Mario Arias:               mario.arias@deri.org
 *   Javier D. Fernandez:       jfergar@infor.uva.es
 *   Miguel A. Martinez-Prieto: migumar2@infor.uva.es
 *   Alejandro Andres:          fuzzy.alej@gmail.com
 */

package org.rdfhdt.hdt.hdt.impl;

import java.io.IOException;

import org.rdfhdt.hdt.dictionary.DictionaryFactory;
import org.rdfhdt.hdt.dictionary.TempDictionary;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.hdt.TempHDT;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.triples.TempTriples;
import org.rdfhdt.hdt.triples.TriplesFactory;
import org.rdfhdt.hdt.util.StopWatch;

/**
 * @author mario.arias, Eugen
 *
 */
public class TempHDTImpl implements TempHDT {

	protected TempDictionary dictionary;
	protected TempTriples triples;

	protected String baseUri = null;
	
	protected ModeOfLoading modeOfLoading = null;
	protected boolean isOrganized = false;

	public TempHDTImpl(HDTOptions spec, String baseUri, ModeOfLoading modeOfLoading) {
		
		this.baseUri = baseUri;
		this.modeOfLoading = modeOfLoading;

		dictionary = DictionaryFactory.createTempDictionary(spec);
		triples = TriplesFactory.createTempTriples(spec);
	}

	@Override
	public TempDictionary getDictionary() {
		return dictionary;
	}

	@Override
	public TempTriples getTriples() {
		return triples;
	}

	public void insert(CharSequence subject, CharSequence predicate, CharSequence object) {
		this.triples.insert(
				dictionary.insert(subject, TripleComponentRole.SUBJECT),
				dictionary.insert(predicate, TripleComponentRole.PREDICATE),
				dictionary.insert(object, TripleComponentRole.OBJECT)
				);
		isOrganized = false;
		modeOfLoading = null;
	}

	@Override
	public void clear() {
		dictionary.clear();
		triples.clear();
		
		isOrganized = false;
	}

	@Override
	public void close() throws IOException {
		dictionary.close();
		triples.close();
	}

	@Override
	public String getBaseURI() {
		return baseUri;
	}

	@Override
	public void reorganizeDictionary(ProgressListener listener) {
		if(isOrganized || dictionary.isOrganized())
			return;

		// Reorganize dictionary
		StopWatch reorgStp = new StopWatch();
		if (ModeOfLoading.ONE_PASS.equals(modeOfLoading)) {
			dictionary.reorganize(triples);
		} else if (ModeOfLoading.TWO_PASS.equals(modeOfLoading)) {
			dictionary.reorganize();
		} else if (modeOfLoading==null) {
			System.err.println("WARNING! Unknown mode of loading from RDF... supposing all triples are in memory " +
					"(meaning HDT not being loaded from RDF -OR- loaded, reorganized and then modified).");
			dictionary.reorganize(triples);
		}
		System.out.println("Dictionary reorganized in "+reorgStp.stopAndShow());

	}

	@Override
	public void reorganizeTriples(ProgressListener listener) {
		if (isOrganized)
			return;
		
		if (!dictionary.isOrganized())
			throw new RuntimeException("Cannot reorganize triples before dictionary is reorganized!");

		// Sort and remove duplicates.
		StopWatch sortDupTime = new StopWatch();
		triples.sort(listener);
		triples.removeDuplicates(listener);
		System.out.println("Sort triples and remove duplicates: "+sortDupTime.stopAndShow());

		isOrganized = true;
	}
	
	public boolean isOrganized() {
		return isOrganized;
	}
}
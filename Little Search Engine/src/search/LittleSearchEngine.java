package search;

import java.io.*;
import java.util.*;

/**
 * This class encapsulates an occurrence of a keyword in a document. It stores the
 * document name, and the frequency of occurrence in that document. Occurrences are
 * associated with keywords in an index hash table.
 * 
 * @author Sesh Venugopal
 * 
 */
class Occurrence {
	/**
	 * Document in which a keyword occurs.
	 */
	String document;
	
	/**
	 * The frequency (number of times) the keyword occurs in the above document.
	 */
	int frequency;
	
	/**
	 * Initializes this occurrence with the given document,frequency pair.
	 * 
	 * @param doc Document name
	 * @param freq Frequency
	 */
	public Occurrence(String doc, int freq) {
		document = doc;
		frequency = freq;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "(" + document + "," + frequency + ")";
	}
}

/**
 * This class builds an index of keywords. Each keyword maps to a set of documents in
 * which it occurs, with frequency of occurrence in each document. Once the index is built,
 * the documents can searched on for keywords.
 *
 */
public class LittleSearchEngine {
	
	/**
	 * This is a hash table of all keywords. The key is the actual keyword, and the associated value is
	 * an array list of all occurrences of the keyword in documents. The array list is maintained in descending
	 * order of occurrence frequencies.
	 */
	HashMap<String,ArrayList<Occurrence>> keywordsIndex;
	
	/**
	 * The hash table of all noise words - mapping is from word to itself.
	 */
	HashMap<String,String> noiseWords;
	
	/**
	 * Creates the keyWordsIndex and noiseWords hash tables.
	 */
	public LittleSearchEngine() {
		keywordsIndex = new HashMap<String,ArrayList<Occurrence>>(1000,2.0f);
		noiseWords = new HashMap<String,String>(100,2.0f);
	}
	
	/**
	 * This method indexes all keywords found in all the input documents. When this
	 * method is done, the keywordsIndex hash table will be filled with all keywords,
	 * each of which is associated with an array list of Occurrence objects, arranged
	 * in decreasing frequencies of occurrence.
	 * 
	 * @param docsFile Name of file that has a list of all the document file names, one name per line
	 * @param noiseWordsFile Name of file that has a list of noise words, one noise word per line
	 * @throws FileNotFoundException If there is a problem locating any of the input files on disk
	 */
	public void makeIndex(String docsFile, String noiseWordsFile) 
	throws FileNotFoundException {
		// load noise words to hash table
		Scanner sc = new Scanner(new File(noiseWordsFile));
		while (sc.hasNext()) {
			String word = sc.next();
			noiseWords.put(word,word);
		}
		
		// index all keywords
		sc = new Scanner(new File(docsFile));
		while (sc.hasNext()) {
			String docFile = sc.next();
			HashMap<String,Occurrence> kws = loadKeyWords(docFile);
			mergeKeyWords(kws);
		}
		
	}

	/**
	 * Scans a document, and loads all keywords found into a hash table of keyword occurrences
	 * in the document. Uses the getKeyWord method to separate keywords from other words.
	 * 
	 * @param docFile Name of the document file to be scanned and loaded
	 * @return Hash table of keywords in the given document, each associated with an Occurrence object
	 * @throws FileNotFoundException If the document file is not found on disk
	 */
	public HashMap<String,Occurrence> loadKeyWords(String docFile) 
	throws FileNotFoundException {
		
		if (docFile == null){
			throw new FileNotFoundException();
		}
		
		HashMap<String,Occurrence> keywords = new HashMap<String,Occurrence>();

		Scanner file = new Scanner(new File(docFile));
		
		while(file.hasNext()){
			String word = getKeyWord(file.next());
			
			if(word != null){ 
				
				if(keywords.get(word) == null){ 
					keywords.put(word, new Occurrence(docFile, 1));
				} else {
					keywords.get(word).frequency++;
				}
			}
		}
		
		return keywords;
	}
	
	/**
	 * Merges the keywords for a single document into the master keywordsIndex
	 * hash table. For each keyword, its Occurrence in the current document
	 * must be inserted in the correct place (according to descending order of
	 * frequency) in the same keyword's Occurrence list in the master hash table. 
	 * This is done by calling the insertLastOccurrence method.
	 * 
	 * @param kws Keywords hash table for a document
	 */
	public void mergeKeyWords(HashMap<String,Occurrence> kws) {
		
		for(String key : kws.keySet()){
			
			if(keywordsIndex.containsKey(key)){
				
				keywordsIndex.get(key).add(kws.get(key));
				insertLastOccurrence(keywordsIndex.get(key));
			} 
			
			else {
				ArrayList<Occurrence> arr = new ArrayList<Occurrence>();
				arr.add(kws.get(key));
				
				keywordsIndex.put(key, arr);

			}
		}
	}
	
	/**
	 * Given a word, returns it as a keyword if it passes the keyword test,
	 * otherwise returns null. A keyword is any word that, after being stripped of any
	 * TRAILING punctuation, consists only of alphabetic letters, and is not
	 * a noise word. All words are treated in a case-INsensitive manner.
	 * 
	 * Punctuation characters are the following: '.', ',', '?', ':', ';' and '!'
	 * 
	 * @param word Candidate word
	 * @return Keyword (word without trailing punctuation, LOWER CASE)
	 */
	public String getKeyWord(String word) {
		
		word = word.toLowerCase();
		
		if(word.length() == 1){
			return null;
		}
		
		while(!Character.isLetter(word.charAt(word.length()-1))){
			char ch = word.charAt(word.length()-1);
			
			if(ch == '.' || ch == ',' || ch == '?' || ch == ':' || ch == ';' || ch == '!'){
				word = word.substring(0, word.length()-1); //delete punctuation one by one
			}
			else {
				return null; // has punctuation other than ones listed
			}
		}
		
		for (int i = 0; i < word.length(); i++){
			if(Character.isLetter(word.charAt(i)) == false){ //punctuation in between
				return null;
			}
		}
		
		if(noiseWords.containsKey(word)){ //noise word
			return null;
		}
		
		return word;
	}
	
	/**
	 * Inserts the last occurrence in the parameter list in the correct position in the
	 * same list, based on ordering occurrences on descending frequencies. The elements
	 * 0..n-2 in the list are already in the correct order. Insertion of the last element
	 * (the one at index n-1) is done by first finding the correct spot using binary search, 
	 * then inserting at that spot.
	 * 
	 * @param occs List of Occurrences
	 * @return Sequence of mid point indexes in the input list checked by the binary search process,
	 *         null if the size of the input list is 1. This returned array list is only used to test
	 *         your code - it is not used elsewhere in the program.
	 */
	public ArrayList<Integer> insertLastOccurrence(ArrayList<Occurrence> occs) {
		
		int n = occs.size();
		int low = 0;
		int high = n-2;
		int mid= 0;
		int target = occs.get(occs.size()-1).frequency;
		
		ArrayList<Integer> midpoints = new ArrayList<Integer>();
		
		//binary search to find midpoints
		while(low <= high){
			
			mid = (high + low)/2;
			
			midpoints.add(mid);
			
			if(occs.get(mid).frequency > target){
				low = mid + 1;
			}
			else if(occs.get(mid).frequency < target) {
				high = mid - 1;
			}
			else{
				break;
			}
		}
		
		
		if(occs.get(mid).frequency > target){
			occs.add(mid+1, occs.get(occs.size()-1));
		}
		else{
			occs.add(mid, occs.get(occs.size()-1));
		}
		occs.remove(occs.size()-1);
		
		return midpoints;
	}
	
	/**
	 * Search result for "kw1 or kw2". A document is in the result set if kw1 or kw2 occurs in that
	 * document. Result set is arranged in descending order of occurrence frequencies. (Note that a
	 * matching document will only appear once in the result.) Ties in frequency values are broken
	 * in favor of the first keyword. (That is, if kw1 is in doc1 with frequency f1, and kw2 is in doc2
	 * also with the same frequency f1, then doc1 will appear before doc2 in the result. 
	 * The result set is limited to 5 entries. If there are no matching documents, the result is null.
	 * 
	 * @param kw1 First keyword
	 * @param kw1 Second keyword
	 * @return List of NAMES of documents in which either kw1 or kw2 occurs, arranged in descending order of
	 *         frequencies. The result size is limited to 5 documents. If there are no matching documents,
	 *         the result is null.
	 */
	public ArrayList<String> top5search(String kw1, String kw2) {
		
		ArrayList<String> documents = null;; 

		ArrayList<Occurrence> Occ1; 
		ArrayList<Occurrence> Occ2; 

		String doc1,doc2; 

		kw1 = kw1.toLowerCase();               
		kw2 = kw2.toLowerCase(); 

		Occ1 = this.keywordsIndex.get(kw1); 
		Occ2 = this.keywordsIndex.get(kw2); 

		if ((Occ1 == null) && (Occ2 == null) ) {
			return null;
			} 


		documents = new ArrayList<String>(); 

		if (Occ1 == null) {       
			for (int i=0 ; i < Occ2.size(); i++) { 
				doc2 = Occ2.get(i).document; 
				documents.add(doc2); 
				if (documents.size() >= (5)) { 
					break; 
				}       
			}  
		} else if (Occ2 == null) {       
			for (int i=0 ; i < (Occ1.size()); i++) { 
				doc1 = Occ1.get(i).document; 
				documents.add(doc1); 
				if (documents.size() >= (5)) { 
					break; 
				} 
			}  
		} else { 
			
			int count=0; 
			Boolean done = false; 

			for (int i = 0; i < ( Occ1.size()) ; i++) { 
				if  (count < Occ2.size()) { 
					

					if  ((Occ1.get(i).frequency)  > (Occ2.get(count).frequency) )  { 

						doc1 = Occ1.get(i).document; 
						if (! documents.contains(doc1)) { 
							documents.add(doc1); 
							doc2=Occ2.get(count).document; 
							if (doc1.equals(doc2)) { 
								count++; 
							} 
							if (documents.size() >= (5)) { 
								done = true; 
								break; 
							}       
						}     
					} else  if ( (Occ1.get(i).frequency) == (Occ2.get(count).frequency) )  { 
						doc1 = Occ1.get(i).document; 
						if (! documents.contains(doc1)) { 
							documents.add(doc1); 
						} 

								doc2=Occ2.get(count).document; 
								if (doc1.equals(doc2)) { 
									count++; 
								} 

								if (documents.size() >= (5)) { 
									done = true; 
									break; 
								}       

					} else if ( (Occ1.get(i).frequency) < (Occ2.get(count).frequency) )  { 

						
						if (count < Occ2.size()) { 
							while (count < Occ2.size()) { 
								if ( (Occ2.get(count).frequency) > (Occ1.get(i).frequency) )  { 
									doc2 = Occ2.get(count).document; 
									if (! documents.contains(doc2)) { 
										documents.add(doc2); 

										count++; 
										if (documents.size() >= (5)) { 
											done = true; 
											break; 
										}       
									} else { 
										count++; 
									} 

								} else { 
									break; 
								} 

							} 
						}  
						if (! done) { 
							doc1 = Occ1.get(i).document; 
							if (! documents.contains(doc1)) { 
								documents.add(doc1); 
							} 

							if (documents.size() >= (5)) { 
								done = true; 
								break; 
							}  
						}
					} 
				} else { 
					
					doc1 = Occ1.get(i).document; 
					if (! documents.contains(doc1)) { 
						documents.add(doc1); 
					} 

					if (documents.size() >= (5)) { 
						done = true; 
						break; 
					}       

				} 

			} 

			for (int j = count ; j < Occ2.size() ; j++) { 
				doc2 = Occ2.get(j).document; 
				if (! documents.contains(doc2)) { 
					documents.add(doc2); 
				} 

				if (documents.size() >= (5)) { 
					done = true; 
					break; 
				}       

			} 
		}   

		printList(documents);     
		
		return documents; 
	}
	
	private void printList(ArrayList<String> arr) { 

		for (String val:arr) { 
			System.out.print(val + " "); 
		} 
		System.out.println();         
	}

}

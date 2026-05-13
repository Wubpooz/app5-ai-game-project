# Project: Query Answering over Linked Data
**BINDA Raphaël, WAHARTE Mathieu**


## Question 1
**Question:** Can you give a Semantic Web method that can answer (one of) the questions proposed in the lecture that cannot be answered by Chatgpt or the question mentioned in Tim Berners-Lee’s TED talk (2009) https://www.ted.com/talks/tim_berners_lee_the_next_web between 11’20 and 12’30 ?

**Response:**  
His question is "What proteins are involved in signal transduction and also related to pyramidal neurons?"  
The course questions are:
- Find that landmark article on data integration written by an Indian researcher in the 1990s.
- Who is the French archer who has won an Olympic event and received the Honor Legion ?
- Identify congress members, who have voted “No” on pro-environmental legislation in the past four years, with high-pollution industry in their congressional districts.
- What are the 20 books that have the most pages, which is lower than 20,000, and that are written by at least one author who has a child ?

We choose "What proteins are involved in signal transduction and also related to pyramidal neurons?"  

To answer this question, we should first build a knowledge graph that contains the relevant information about proteins, signal transduction, and pyramidal neurons. The proteins will then have relationships to both SignalTransduction and Pyramidal Neurons. However, these relationships may not be a direct link. The datamodel may link them to both through other steps such as reactions. Therefore, we should first probe the datamodel for the relationships between these entities. We can use SPARQL queries to explore the relationships in the knowledge graph and find the relevant proteins.  

Finally, we can answer the question with a final SPARQL query such as this one:  
```sparql
SELECT ?p WHERE {
  ?p a Protein . 
  ?p involvedIn SignalTransduction .
  ?p inside ?r .
  ?r a Reaction .
  ?r about ?s .
  ?s a PyramidalNeurons .
}
```

After some research, we found a realistic version of this query (this was found with the help of an IA but it follows our logic):  
```sparql
PREFIX up:     <http://purl.uniprot.org/core/>
PREFIX go:     <http://purl.obolibrary.org/obo/>
PREFIX lscr:   <http://purl.org/lscr#>
PREFIX genex:  <http://purl.org/genex#>
PREFIX orth:   <http://purl.org/net/orth#>
PREFIX obo:    <http://purl.obolibrary.org/obo/>
PREFIX rdfs:   <http://www.w3.org/2000/01/rdf-schema#>


SELECT DISTINCT ?protein ?proteinName ?goTerm ?goLabel WHERE {
  # --- Protein involved in signal transduction ---
  # ?p involvedIn SignalTransduction
  SERVICE <https://sparql.uniprot.org/sparql> {
    ?protein a up:Protein ;
             up:recommendedName/up:fullName ?proteinName ;
             up:classifiedWith ?goTerm .

    ?goTerm rdfs:subClassOf* go:GO_0007165 . # signal transduction
    ?goTerm rdfs:label ?goLabel .
  }

  # --- Gene expressed in pyramidal neurons ---
  # ?p inside PyramidalNeurons
  SERVICE <https://www.bgee.org/sparql> {
    ?gene a orth:Gene ;
          genex:isExpressedIn ?cond .

    ?cond genex:hasAnatomicalEntity ?anatEntity .
    ?anatEntity rdfs:label "pyramidal neuron"@en .

    ?gene lscr:xrefUniprot ?protein .
  }
}
```

The `SERVICE` keyword allows us to query multiple SPARQL endpoints in a single query.  




## Question 2
**Question:** Propose another (complex) question in natural language and give your answer by checking Linked Data (and combined with the help of Chatgpt or LLM-large language model-based methods).


## Question 3
**Question:** Can you explain the answers? For answers from Linked Data, you can use "CONSTRUCT" to return the "justifications" of your answers. And how about the hybrid approach by combining Linked Data and Chatgpt ? You may refer to DBpedia, Wikidata, or other linked data, such as https://lod-cloud.net, https://linkedlifedata.com/, https://www.ontotext.com/knowledgehub/publications/linked-life-data-knowledge-extraction-semantic-data-integration-pharmaceutical-domain/.




## Report
You need to report (at least 3 pages) of your work on this project. The report should contain the work’s context introduction, the problem statement, the method, and the solution. 
Moreover, you need to develop in the report, the process of getting the answers to the two questions you have selected from the following perspectives (but not limited to) :
- Which datasets have you explored ? What queries have you tried ?
- What difficulties were there ? And how have you solved them ?
- Do you see some benefits and limitations of the techniques ?
- Can you justify the reason for your answers ?


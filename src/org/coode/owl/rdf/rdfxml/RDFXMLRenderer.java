package org.coode.owl.rdf.rdfxml;

import static org.semanticweb.owl.vocab.OWLRDFVocabulary.RDF_DESCRIPTION;

import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.coode.owl.rdf.model.RDFLiteralNode;
import org.coode.owl.rdf.model.RDFNode;
import org.coode.owl.rdf.model.RDFResourceNode;
import org.coode.owl.rdf.model.RDFTriple;
import org.coode.owl.rdf.renderer.RDFRendererBase;
import org.coode.xml.OWLOntologyNamespaceManager;
import org.coode.xml.XMLWriterFactory;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLDataProperty;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLObjectProperty;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyFormat;
import org.semanticweb.owl.model.OWLOntologyManager;
import org.semanticweb.owl.util.VersionInfo;
import org.semanticweb.owl.vocab.OWLRDFVocabulary;


/**
 * The current class of OWL API has been twicked a bit since it added quite a lot of spaces 
 * in the generated owl files making them unnecessarily large
 * 
 * 
 * Author: Matthew Horridge<br> The University Of Manchester<br> Bio-Health Informatics Group<br> Date:
 * 06-Dec-2006<br><br>
 */
public class RDFXMLRenderer extends RDFRendererBase {

    private RDFXMLWriter writer;

    private Set<RDFResourceNode> pending;


    public RDFXMLRenderer(OWLOntologyManager manager, OWLOntology ontology, Writer w) {
        this(manager, ontology, w, manager.getOntologyFormat(ontology));
    }


    public RDFXMLRenderer(OWLOntologyManager manager, OWLOntology ontology, Writer w, OWLOntologyFormat format) {
        super(ontology, manager, format);
        pending = new HashSet<RDFResourceNode>();
        writer = new RDFXMLWriter(XMLWriterFactory.getInstance().createXMLWriter(w,
                                                                                 new OWLOntologyNamespaceManager(manager,
                                                                                                                 ontology,
                                                                                                                 format),
                                                                                 ontology.getURI().toString()));
        prettyPrintedTypes = new HashSet<URI>();
        prettyPrintedTypes.add(OWLRDFVocabulary.OWL_CLASS.getURI());
        prettyPrintedTypes.add(OWLRDFVocabulary.OWL_OBJECT_PROPERTY.getURI());
        prettyPrintedTypes.add(OWLRDFVocabulary.OWL_DATA_PROPERTY.getURI());
        prettyPrintedTypes.add(OWLRDFVocabulary.OWL_ANNOTATION_PROPERTY.getURI());
        prettyPrintedTypes.add(OWLRDFVocabulary.OWL_RESTRICTION.getURI());
        prettyPrintedTypes.add(OWLRDFVocabulary.OWL_THING.getURI());
        prettyPrintedTypes.add(OWLRDFVocabulary.OWL_NOTHING.getURI());
        prettyPrintedTypes.add(OWLRDFVocabulary.OWL_ONTOLOGY.getURI());
        prettyPrintedTypes.add(OWLRDFVocabulary.OWL_NEGATIVE_DATA_PROPERTY_ASSERTION.getURI());
        prettyPrintedTypes.add(OWLRDFVocabulary.OWL_NEGATIVE_OBJECT_PROPERTY_ASSERTION.getURI());
    }


    protected void beginDocument() {
        writer.startDocument();
    }


    protected void endDocument() {
        writer.endDocument();
        writer.writeComment(VersionInfo.getVersionInfo().getGeneratedByMessage());
    }


    protected void writeIndividualComments(OWLIndividual ind) {
//        writer.writeComment(EscapeUtils.escapeXML(ind.getURI().toString()));
    }


    protected void writeClassComment(OWLClass cls) {
//        writer.writeComment(EscapeUtils.escapeXML(cls.getURI().toString()));
    }


    protected void writeDataPropertyComment(OWLDataProperty prop) {
//        writer.writeComment(EscapeUtils.escapeXML(prop.getURI().toString()));
    }


    protected void writeObjectPropertyComment(OWLObjectProperty prop) {
//        writer.writeComment(EscapeUtils.escapeXML(prop.getURI().toString()));
    }


    protected void writeBanner(String name) {
//        writer.writeComment(
//                "\n///////////////////////////////////////////////////////////////////////////////////////\n" + "//\n" + "// " + name + "\n" + "//\n" + "///////////////////////////////////////////////////////////////////////////////////////\n");
    }


    public void render(RDFResourceNode node) {
        if (pending.contains(node)) {
            // We essentially remove all structure sharing during parsing - any cycles therefore indicate a bug!
//            throw new IllegalStateException("Rendering cycle!  This indicates structure sharing and should not happen! (Node: " + node.toString() + ")");
            return;
        }
        pending.add(node);
        Set<RDFTriple> triples = new TreeSet<RDFTriple>(new TripleComparator());
        triples.addAll(getGraph().getTriplesForSubject(node));
        RDFTriple candidatePrettyPrintTypeTriple = null;
        for (RDFTriple triple : getGraph().getTriplesForSubject(node)) {
            URI propertyURI = triple.getProperty().getURI();
            if (propertyURI.equals(OWLRDFVocabulary.RDF_TYPE.getURI()) && !triple.getObject().isAnonymous()) {
                if (OWLRDFVocabulary.BUILT_IN_VOCABULARY.contains(triple.getObject().getURI())) {
                    if (prettyPrintedTypes.contains(triple.getObject().getURI())) {
                        candidatePrettyPrintTypeTriple = triple;
                    }
                }
                else {
                    candidatePrettyPrintTypeTriple = triple;
                }
            }
        }
        if (candidatePrettyPrintTypeTriple == null) {
            writer.writeStartElement(RDF_DESCRIPTION.getURI());
        }
        else {
            writer.writeStartElement(candidatePrettyPrintTypeTriple.getObject().getURI());
        }
        if (!node.isAnonymous()) {
            writer.writeAboutAttribute(node.getURI());
        }
        for (RDFTriple triple : triples) {
            if (candidatePrettyPrintTypeTriple != null && candidatePrettyPrintTypeTriple.equals(triple)) {
                continue;
            }
            writer.writeStartElement(triple.getProperty().getURI());
            RDFNode objectNode = triple.getObject();
            if (!objectNode.isLiteral()) {
                RDFResourceNode objectRes = (RDFResourceNode) objectNode;
                if (objectRes.isAnonymous()) {
                    // Special rendering for lists
                    if (isObjectList(objectRes)) {
                        writer.writeParseTypeAttribute();
                        List<RDFNode> list = new ArrayList<RDFNode>();
                        toJavaList(objectRes, list);
                        for (RDFNode n : list) {
                            if (n.isAnonymous()) {
                                render((RDFResourceNode) n);
                            }
                            else {
                                if (n.isLiteral()) {
                                    RDFLiteralNode litNode = (RDFLiteralNode) n;
                                    writer.writeStartElement(OWLRDFVocabulary.RDFS_LITERAL.getURI());
                                    if (litNode.getDatatype() != null) {
                                        writer.writeDatatypeAttribute(litNode.getDatatype());
                                    }
                                    else if (litNode.getLang() != null) {
                                        writer.writeLangAttribute(litNode.getLang());
                                    }
                                    writer.writeTextContent((litNode.getLiteral()));
                                    writer.writeEndElement();
                                }
                                else {
                                    writer.writeStartElement(RDF_DESCRIPTION.getURI());
                                    writer.writeAboutAttribute(n.getURI());
                                    writer.writeEndElement();
                                }
                            }
                        }
                    }
                    else {
                        render(objectRes);
                    }
                }
                else {
                    writer.writeResourceAttribute(objectRes.getURI());
                }
            }
            else {
                RDFLiteralNode rdfLiteralNode = ((RDFLiteralNode) objectNode);
                if (rdfLiteralNode.getDatatype() != null) {
                    writer.writeDatatypeAttribute(rdfLiteralNode.getDatatype());
                }
                else if (rdfLiteralNode.getLang() != null) {
                    writer.writeLangAttribute(rdfLiteralNode.getLang());
                }
                writer.writeTextContent(rdfLiteralNode.getLiteral());
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
        pending.remove(node);
    }
}

package com.example.photoarchive.experiment;


import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.util.Iterator;

public class MetadataJSON {

    //	referenced
    //	http://johnbokma.com/java/obtaining-image-metadata.html
    //	changed from XML to JSON DOM tree

    StringBuilder json = new StringBuilder();

    public StringBuilder getJson() { return json; }

    public MetadataJSON( String fileName ) {
        try { readAndDisplayMetadata( fileName ); }
        catch( Exception e ) { e.printStackTrace(); }
    }

    void readAndDisplayMetadata( String fileName ) {
        try {
            File file = new File( fileName );
            ImageInputStream iis = ImageIO.createImageInputStream(file);
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);

            if (readers.hasNext()) {

                // pick the first available ImageReader
                ImageReader reader = readers.next();

                // attach source to the reader
                reader.setInput(iis, true);

                // read metadata of first image
                IIOMetadata metadata = reader.getImageMetadata(0);

                json.append( "{" );
                String[] names = metadata.getMetadataFormatNames();
                int length = names.length;
                for (int i = 0; i < length; i++) {
                    if ( i>0 ) { json.append( ",\n" ); }
                    json.append( displayMetadata( metadata.getAsTree( names[i] )));
                }
                json.append( "}" );
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    String displayMetadata(Node root) {
        return displayMetadata(root, 0);
    }

    void indent(StringBuilder sb, int level) {
        for (int i = 0; i < level; i++)
            sb.append( "    " );
    }

    String displayMetadata(Node node, int level) {
        // print open tag of element
        StringBuilder sb = new StringBuilder();
        //indent(sb, level);
        sb.append( "\""+ node.getNodeName() +"\": [\n" );
        indent(sb, level +1);
        NamedNodeMap map = node.getAttributes();
        if (map != null && map.getLength() != 0) {

            // print attribute values
            sb.append( "{ " );
            int length = map.getLength();
            for (int i = 0; i < length; i++) {
                Node attr = map.item(i);

                // Remove quotes from numbers
                String val = "";
                String nodeValue = attr.getNodeValue();
                try {
                    Double.parseDouble(nodeValue);
                    if ( nodeValue.equals( "NaN" ) || nodeValue.equals( "Infinity" )) throw new NumberFormatException();
                    val = nodeValue;
                } catch(NumberFormatException e) {
                    //System.out.println("wat? "+ nodeValue);
                    val = "\"" + nodeValue + "\"";
                }

                if ( i>0 ) { sb.append(", "); }
                sb.append("\"" + attr.getNodeName() + "\": "+ val.replaceAll( "\\p{Cc}","" ));
            }
            sb.append( " }" );
        } else { sb.append( "null" ); }

        Node child = node.getFirstChild();
        while (child != null) {
            // print children recursively
            sb.append( ",\n");
            indent(sb, level +1);
            sb.append( "{"+ displayMetadata(child, level + 1) +"}" );
            child = child.getNextSibling();
        }

        sb.append("\n");
        // print close tag of element
        indent(sb, level);
        sb.append("]");
        return sb.toString();
    }
}










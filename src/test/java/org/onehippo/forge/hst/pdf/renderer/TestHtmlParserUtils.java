/**
 * Copyright 2012 Hippo.
 * 
 * This file is part of HST PDF Renderer.
 * 
 * HST PDF Renderer is free software: you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by the Free 
 * Software Foundation, either version 3 of the License, or (at your option) 
 * any later version.
 * 
 * HST PDF Renderer is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * HST PDF Renderer. If not, see http://www.gnu.org/licenses/.
 */
package org.onehippo.forge.hst.pdf.renderer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;

import org.cyberneko.html.parsers.DOMParser;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

public class TestHtmlParserUtils {

    private String html = "<HTML><H1>Hello, World!</H1><HR><BR><P>Some more text...</HTML>";

    @Test
    public void testHtmlParsing() throws Exception {
        DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));
        Document document = parser.getDocument();

        Element root = document.getDocumentElement();
        assertEquals("HTML", root.getNodeName());
        assertTrue(root.getElementsByTagName("H1").getLength() > 0);

        Element h1 = (Element) root.getElementsByTagName("H1").item(0);
        assertEquals("Hello, World!", h1.getTextContent());

        assertTrue(root.getElementsByTagName("HR").getLength() > 0);
        assertTrue(root.getElementsByTagName("BR").getLength() > 0);
        assertTrue(root.getElementsByTagName("P").getLength() > 0);

        Element p = (Element) root.getElementsByTagName("P").item(0);
        assertEquals("Some more text...", p.getTextContent());
    }

}

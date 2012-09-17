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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.URI;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.lowagie.text.DocumentException;

/**
 * HtmlPDFRenderer
 * <P>
 * This class is designed to be used as singleton object.
 * So, {@link #renderHtmlToPDF(InputStream, String, boolean, OutputStream)} and {@link #renderHtmlToPDF(Reader, boolean, OutputStream)} should be thread-safe.
 * </P>
 */
public class HtmlPDFRenderer {

    private static Logger log = LoggerFactory.getLogger(HtmlPDFRenderer.class);

    private URI cssURI;
    private int bufferSize = 4096;
    private UserAgentCallback userAgentCallback;

    public HtmlPDFRenderer() {
    }

    public URI getCssURI() {
        return cssURI;
    }

    public void setCssURI(URI cssURI) {
        this.cssURI = cssURI;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public UserAgentCallback getUserAgentCallback() {
        return userAgentCallback;
    }

    public void setUserAgentCallback(UserAgentCallback userAgentCallback) {
        this.userAgentCallback = userAgentCallback;
    }

    public void renderHtmlToPDF(InputStream htmlInput, String inputHtmlEncoding, boolean convertToXHTML, OutputStream pdfOutput, String baseURL) throws IOException {
        InputStreamReader htmlReader = new InputStreamReader(htmlInput, inputHtmlEncoding);
        renderHtmlToPDF(htmlReader, convertToXHTML, pdfOutput, baseURL);
    }

    public void renderHtmlToPDF(Reader htmlInput, boolean convertToXHTML, OutputStream pdfOutput, String baseURL) throws IOException {
        Reader xhtmlReader = null;

        try {
            if (convertToXHTML) {
                xhtmlReader = convertHtmlReaderToXhtmlReader(htmlInput);
            } else {
                xhtmlReader = htmlInput;
            }

            ITextRenderer renderer = new ITextRenderer();

            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(new InputSource(xhtmlReader));

            removeCssLinkElementsFromXhtmlDocument(document);

            if (cssURI != null) {
                addCssLinkElementToXhtmlDocument(document, cssURI);
            }

            if (userAgentCallback != null) {
                renderer.getSharedContext().setUserAgentCallback(userAgentCallback);
            }
            
            renderer.setDocument(document, baseURL);
            renderer.layout();
            renderer.createPDF(pdfOutput);
        } catch (ParserConfigurationException e) {
            log.error("Parse configuration exception.", e);
        } catch (SAXException e) {
            log.error("XML parsing exception.", e);
        } catch (DocumentException e) {
            log.error("pdf generation exception.", e);
        } finally {
            if (xhtmlReader != htmlInput) {
                IOUtils.closeQuietly(xhtmlReader);
            }
        }
    }

    private Reader convertHtmlReaderToXhtmlReader(Reader htmlReader) throws IOException {
        Tidy tidy = new Tidy();

        tidy.setMakeClean(true);
        tidy.setXHTML(true);
        tidy.setDocType("omit");
        tidy.setNumEntities(true);
        tidy.setInputEncoding("UTF-8");
        tidy.setQuiet(true);
        tidy.setShowWarnings(false);

        ByteArrayOutputStream tidyOut = null;
        OutputStreamWriter osw = null;
        byte [] bytes = null;

        try {
            tidyOut = new ByteArrayOutputStream(bufferSize);
            osw = new OutputStreamWriter(tidyOut, "UTF-8");
            tidy.parse(htmlReader, osw);
            osw.flush();
            bytes = tidyOut.toByteArray();
        } finally {
            IOUtils.closeQuietly(osw);
            IOUtils.closeQuietly(tidyOut);
        }

        return new InputStreamReader(new ByteArrayInputStream(bytes), "UTF-8");
    }

    private static void removeCssLinkElementsFromXhtmlDocument(Document document) {
        Element html = document.getDocumentElement();
        NodeList childNodeList = html.getChildNodes();
        Element headElem = null;

        if (childNodeList != null) {
            int length = childNodeList.getLength();

            for (int i = 0; i < length; i++) {
                Node childNode = childNodeList.item(i);

                if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                    if (StringUtils.equalsIgnoreCase("head", childNode.getNodeName())) {
                        headElem = (Element) childNode;
                        break;
                    }
                }
            }
        }

        if (headElem != null) {
            childNodeList = headElem.getChildNodes();

            if (childNodeList != null) {
                int length = childNodeList.getLength();

                for (int i = length - 1; i >= 0; i--) {
                    Node childNode = childNodeList.item(i);

                    if (childNode.getNodeType() == Node.ELEMENT_NODE && StringUtils.equalsIgnoreCase("link", childNode.getNodeName())) {
                        headElem.removeChild(childNode);
                    }
                }
            }
        }
    }

    private static void addCssLinkElementToXhtmlDocument(Document document, URI cssURI) {
        Element html = document.getDocumentElement();
        NodeList childNodeList = html.getChildNodes();
        Element firstChildElem = null;
        Element headElem = null;

        if (childNodeList != null) {
            int length = childNodeList.getLength();

            for (int i = 0; i < length; i++) {
                Node childNode = childNodeList.item(i);

                if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                    if (firstChildElem == null) {
                        firstChildElem = (Element) childNode;
                    }

                    if (StringUtils.equalsIgnoreCase("head", childNode.getNodeName())) {
                        headElem = (Element) childNode;
                        break;
                    }
                }
            }
        }

        if (headElem == null) {
            headElem = document.createElement("head");
            html.insertBefore(headElem, firstChildElem);
        }

        Element linkElem = document.createElement("link");
        linkElem.setAttribute("type", "text/css");
        linkElem.setAttribute("rel", "stylesheet");
        linkElem.setAttribute("href", cssURI.toString());
        linkElem.setAttribute("media", "print");

        headElem.appendChild(linkElem);
    }
}

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

/**
 * TestHtmlPDFRenderer
 */
public class TestHtmlPDFRenderer {

    private HtmlPDFRenderer pdfRenderer;

    @Before
    public void setUp() throws Exception {
        pdfRenderer = new HtmlPDFRenderer();
        File cssFile = FileUtils.toFile(getClass().getResource("alice.css"));
        pdfRenderer.setCssURI(cssFile.toURI());
    }

    @Test
    public void testHtmlToPDF() throws Exception {
        String htmlFileName = "solar-power-the-sky-is-the-limit.html";
        InputStream htmlInput = null;
        OutputStream pdfOutput = null;

        try {
            htmlInput = getClass().getResourceAsStream(htmlFileName);
            pdfOutput = new FileOutputStream(new File(new File("target"), htmlFileName + ".pdf"));
            pdfRenderer.renderHtmlToPDF(htmlInput, "UTF-8", true, pdfOutput, "http://localhost:8080/");
        } finally {
            IOUtils.closeQuietly(pdfOutput);
            IOUtils.closeQuietly(htmlInput);
        }
    }
}

package org.onehippo.forge.hstpdfrenderer.demo.beans;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoHtml;

@Node(jcrType="hstpdfrendererdemo:textdocument")
public class TextDocument extends BaseDocument{
    
    public String getTitle() {
        return getProperty("hstpdfrendererdemo:title");
    }

    public String getSummary() {
        return getProperty("hstpdfrendererdemo:summary");
    }
    
    public HippoHtml getHtml(){
        return getHippoHtml("hstpdfrendererdemo:body");    
    }

}

package org.onehippo.forge.hstpdfrenderer.demo.beans;

import java.util.Calendar;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSetBean;

@Node(jcrType="hstpdfrendererdemo:newsdocument")
public class NewsDocument extends BaseDocument{

    public String getTitle() {
        return getProperty("hstpdfrendererdemo:title");
    }
    
    public String getSummary() {
        return getProperty("hstpdfrendererdemo:summary");
    }
    
    public Calendar getDate() {
        return getProperty("hstpdfrendererdemo:date");
    }

    public HippoHtml getHtml(){
        return getHippoHtml("hstpdfrendererdemo:body");    
    }

    /**
     * Get the imageset of the newspage
     *
     * @return the imageset of the newspage
     */
    public HippoGalleryImageSetBean getImage() {
        return getLinkedBean("hstpdfrendererdemo:image", HippoGalleryImageSetBean.class);
    }


}

package org.apache.wicket.testapplication;

import org.apache.wicket.Application;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class TestPage2 extends WebPage
{
    private static final long serialVersionUID = 1L;

    public TestPage2(final PageParameters parameters )
    {
        super( parameters );
    }

    @Override
    protected void onBeforeRender()
    {
        super.onBeforeRender();
    }

    @Override
    protected void onDetach()
    {
        super.onDetach();

        System.out.println( "Page detaching" );
    }
}

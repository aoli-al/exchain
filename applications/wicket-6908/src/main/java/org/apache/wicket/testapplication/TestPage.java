package org.apache.wicket.testapplication;

import org.apache.wicket.Application;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class TestPage extends WebPage
{
    private static final long serialVersionUID = 1L;
    private LDM ldm = new LDM();


    public TestPage( final PageParameters parameters )
    {
        super( parameters );
        Application.get().getRequestCycleSettings().setExceptionRetryCount(0);

        add( new Label( "renderCount", this::getRenderCount ) );
        add( new Label( "model", ldm ) );

        add( new ExternalLink( "stale", () -> {
            var baseUrl = urlFor( new RenderPageRequestHandler( getPage() ) );
            return baseUrl + "-999.-btn";
        } ) );
    }

    @Override
    protected void onBeforeRender()
    {
        System.out.println( "Model still attached? " + ldm.isAttached() );
        if (ldm.isAttached()) {
            throw new RuntimeException("model still attached!");
        }

        super.onBeforeRender();
    }

    @Override
    protected void onDetach()
    {
        super.onDetach();

        System.out.println( "Page detaching" );
    }

    private class LDM extends LoadableDetachableModel<String>
    {
        @Override
        protected String load()
        {
            return "Dummy";
        }
    }
}

package org.apache.wicket.myapplication;

import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class HomePage3 extends WebPage
{
    private static final long serialVersionUID = 1L;
    private LDM ldm = new LDM();

    public HomePage3(final PageParameters parameters )
    {
        super( parameters );

        add( new Label( "renderCount", this::getRenderCount ) );
        add( new Label( "model", ldm ) );

        add( new ExternalLink( "stale", () -> {
            var baseUrl = urlFor( new RenderPageRequestHandler( getPage() ) );
            return baseUrl + "-999.-btn";
        } ) );

        add( new Link<Void>( "test" )
        {
            @Override
            public void onClick()
            {
                setResponsePage( new TestPage( null ) );
            }
        } );
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

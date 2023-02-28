package org.apache.wicket.testapplication;

import org.apache.wicket.Application;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.core.request.mapper.StalePageException;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class TestPage extends WebPage
{
    private static final long serialVersionUID = 1L;
    private LDM ldm = new LDM();
    private boolean staleLinkClicked = false;


    public TestPage( final PageParameters parameters )
    {
        super( parameters );

        add( new Label( "renderCount", this::getRenderCount ) );
        add( new Label( "model", ldm ) );

        add( new ExternalLink( "stale", () -> {
            var baseUrl = urlFor( new RenderPageRequestHandler( getPage() ) );
            return baseUrl + "-999.-btn";
        } ) );
        add( new Link<Void>( "exception" )
        {
            @Override
            public void onClick()
            {
                staleLinkClicked = true;
                throw new StalePageException(TestPage.this);
            }
        } );

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

    @Override
    protected void onRemove() {
        super.onRemove();
    }

    @Override
    public void onEvent(IEvent<?> event) {
        super.onEvent(event);
    }

    private class LDM extends LoadableDetachableModel<String>
    {
        public boolean canThrow = true;
        public LDM() {
            super();
        }
        @Override
        protected String load()
        {
            return "Dummy";
        }


        @Override
        public void detach() {
            super.detach();
        }
    }
}

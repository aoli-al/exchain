package org.apache.wicket.testapplication;

import org.apache.wicket.Application;
import org.apache.wicket.DefaultPageManagerProvider;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.page.PageManager;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.settings.ExceptionSettings;

public class HomePage extends WebPage
{
    private static final long serialVersionUID = 1L;
    private LDM ldm = new LDM();

    public HomePage( final PageParameters parameters )
    {
        super( parameters );
        Application.get().getRequestCycleSettings().setExceptionRetryCount(0);
        Application.get().getExceptionSettings().setUnexpectedExceptionDisplay(ExceptionSettings.SHOW_NO_EXCEPTION_PAGE);

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

        add( new Link<Void>( "test2" )
        {
            @Override
            public void onClick()
            {
                setResponsePage( new TestPage2( null ) );
            }
        } );
    }

    @Override
    protected void onBeforeRender()
    {
        System.out.println( "Model still attached? " + ldm.isAttached() );

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

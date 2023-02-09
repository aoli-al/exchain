package org.apache.wicket.testapplication;

import org.apache.wicket.core.request.handler.IPageProvider;
import org.apache.wicket.core.request.handler.PageProvider;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;


public class TestPage extends WebPage
{
    private static final long serialVersionUID = 1L;
    public boolean added = false;
    public TestPage() {
        this(null);
    }


    public TestPage( final PageParameters parameters )
    {
        super( parameters );

        add( new ExternalLink("stale", new IModel<String>() {
            @Override
            public String getObject() {
                var baseUrl = urlFor( new RenderPageRequestHandler(new PageProvider(getPage())));
                var url = baseUrl + "-999.-btn";
                return url;
            }

            @Override
            public void setObject(String object) {

            }

            @Override
            public void detach() {

            }
        }));
        add(new Link<Void>("test2") {

            @Override
            public void onClick() {
                setResponsePage(new TestPage2(null));
            }
        });

    }

    @Override
    protected void onBeforeRender()
    {
        System.out.println( "Model still attached? ");

        super.onBeforeRender();
    }

    @Override
    protected void onDetach()
    {
        super.onDetach();

        System.out.println( "Page detaching" );
    }
}

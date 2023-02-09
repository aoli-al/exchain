package org.apache.wicket.testapplication;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.request.mapper.parameter.PageParameters;


public class TestPage2 extends WebPage
{
    private static final long serialVersionUID = 1L;
    public boolean added = false;
    public TestPage2() {
        this(null);
    }


    public TestPage2(final PageParameters parameters )
    {
        super( parameters );

//        var baseUrl = urlFor( new RenderPageRequestHandler( getPage() ) );
//        var url = baseUrl + "-999.-btn";
//        add( new ExternalLink( "stale", () -> {
//        } ) );

        add(new Link<Void>("test") {

            @Override
            public void onClick() {
                setResponsePage(new TestPage(null));
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

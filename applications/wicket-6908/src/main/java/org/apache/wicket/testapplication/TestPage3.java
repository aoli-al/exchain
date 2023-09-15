package org.apache.wicket.testapplication;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class TestPage3 extends WebPage
{
    private static final long serialVersionUID = 1L;

    public TestPage3(final PageParameters parameters )
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

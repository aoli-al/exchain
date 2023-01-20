package org.apache.wicket.testapplication;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class HomePage extends WebPage {
    private static final long serialVersionUID = 1L;
    public static ExceptionalLoad ldm = new ExceptionalLoad();

    static class SpecificException extends RuntimeException {

    }


    static class ExceptionalLoad extends LoadableDetachableModel<String>
    {
        public boolean detachCalled = false;
        @Override
        protected String load()
        {
            throw new SpecificException();
        }
        @Override
        protected void onDetach()
        {
            detachCalled = true;
        }
    }

    class LoadWrapper extends LoadableDetachableModel<String> {
        ExceptionalLoad ldm = HomePage.ldm;
        @Override
        protected String load() {
            try {
                String result = ldm.getObject();
                return result.toLowerCase();
            } catch (SpecificException e) {
                return "exception happened!";
            }
        }

        @Override
        public void detach() {
            super.detach();
            ldm.detach();
//            if (!ldm.detachCalled) {
//                throw new RuntimeException("ldm is not detached!");
//            }
        }
    }

    LoadWrapper wrapper = new LoadWrapper();

    public HomePage(final PageParameters parameters) {
        super(parameters);

        add(new Label("version", getApplication().getFrameworkSettings().getVersion()));
        add(new Label("model", wrapper));
    }

}

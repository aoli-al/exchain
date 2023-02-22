package org.apache.wicket.myapplication;

import org.apache.wicket.myapplication.model.JpaLoadableModel;
import org.apache.wicket.myapplication.model.Person;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class TestPage extends WebPage
{
    private static final long serialVersionUID = 1L;
    private LDM ldm = new LDM();

    public EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("org.hibernate.tutorial" +
            ".jpa");
    public EntityManager manager = entityManagerFactory.createEntityManager();
    public Person person = new Person("a", "b", "c");
    public JpaLoadableModel<Person> model = new JpaLoadableModel(entityManagerFactory, person);
    public boolean added = false;


    public TestPage( final PageParameters parameters )
    {
        super( parameters );


        manager.getTransaction().begin();
        manager.persist(person);
        manager.getTransaction().commit();
        add( new Label( "renderCount", this::getRenderCount ) );
        add( new Label( "model", new PropertyModel(model, "name")) );

        add( new ExternalLink( "stale", () -> {
            var baseUrl = urlFor( new RenderPageRequestHandler( getPage() ) );
            return baseUrl + "-999.-btn";
        } ) );
        add( new ExternalLink( "home", () -> {
            if (added) {
                manager.getTransaction().begin();
                manager.remove(person);
                manager.getTransaction().commit();
            } else {
                manager.getTransaction().begin();
                person.setName(getRenderCount() + "FOO");
                manager.persist(person);
                manager.getTransaction().commit();
            }
            var baseUrl = urlFor( new RenderPageRequestHandler( getPage() ) );
            return baseUrl + "-999.-btn";
        } ) );
    }

    @Override
    protected void onBeforeRender()
    {
        System.out.println( "Model still attached? " + model.isAttached() );

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

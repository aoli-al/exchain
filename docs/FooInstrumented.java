public class Foo {
    Taint thisTag;
    void localVars() {
        int a = 0;
        Taint a_tag;
        Object b = null;
        Taint b_tag;
        Foo c = new Foo();
        Taint c_tag;
        c.thisTag = "some tag";
    }
}
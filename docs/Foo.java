public class Foo {
    int[] array;
    TaggedArray array_wrapper;


    Foo() {
        array_wrapper = new TaggedArray(array);
    }

    void localVars() {
        int a = 0;
        Object b = null;
        Foo c = new Foo();
    }
}
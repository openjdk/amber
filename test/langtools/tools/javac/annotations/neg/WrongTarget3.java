/*
 * @test /nodynamiccopyright/
 * @compile/fail/ref=WrongTarget3.out -XDrawDiagnostics WrongTarget3.java
 * @enablePreview
 */

import static java.lang.annotation.ElementType.DECONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;

@java.lang.annotation.Target({METHOD})
@interface Wrong {
}

@java.lang.annotation.Target({DECONSTRUCTOR})
@interface OK {
}

public class WrongTarget3 {

    @Wrong
    public pattern WrongTarget3(String name) {
        match WrongTarget3("");
    }

    @OK
    public pattern WrongTarget3(String name, int i) {
        match WrongTarget3("", 42);
    }
}

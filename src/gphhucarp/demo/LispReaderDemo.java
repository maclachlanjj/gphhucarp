package gphhucarp.demo;

import ec.gp.GPTree;
import gphhucarp.gp.UCARPPrimitiveSet;
import gputils.LispUtils;

/**
 * A demo for lisp reader.
 * Given a string lisp expression, one can first simplify the expression,
 * then parse the string into a GPTree class, and print it in a Graphviz format.
 */
public class LispReaderDemo {
    public static void main(String[] args) {
        String expression =
                "(+ (min (- DC FRT) CFH) (max (min (/ (- DC FRT) (+ CTD RQ)) (/ 0.11144301662719736 RQ)) (* (max (* (- DC FRT) (- (* CFH CTD) (+ FUT CFD))) (/ CFR1 CTD)) (max (+ (* (min RQ1 CR) (+ CTD RQ)) (- DC FRT)) (+ CTD RQ)))))";

        expression = LispUtils.simplifyExpression(expression);

        GPTree gpTree = LispUtils.parseExpression(expression, UCARPPrimitiveSet.wholePrimitiveSet());
        System.out.println(gpTree.child.makeGraphvizTree());
    }
}

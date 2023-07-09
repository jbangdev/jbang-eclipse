///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS https://foo.bar/missing.jar

class brokendeps3 {

    public static void main(String... args) throws Exception {
        System.out.println("Hello " + ((args.length>0)?args[0]:"jbang"));
    }
}

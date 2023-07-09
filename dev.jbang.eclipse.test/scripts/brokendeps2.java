///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS missing.jar

class brokendeps2 {

    public static void main(String... args) throws Exception {
        System.out.println("Hello " + ((args.length>0)?args[0]:"jbang"));
    }
}

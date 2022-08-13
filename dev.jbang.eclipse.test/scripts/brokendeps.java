///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.github.lalyos:jfiglet:6.6.6

import com.github.lalyos.jfiglet.FigletFont;

class brokendeps {

    public static void main(String... args) throws Exception {
        System.out.println(FigletFont.convertOneLine(
               "Hello " + ((args.length>0)?args[0]:"jbang")));  ;;
    }
}

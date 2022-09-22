import com.github.lalyos.jfiglet.FigletFont;
/**
 * bar is the base
 */

class bar {

    public static void main(String... args) throws Exception {
        System.out.println(FigletFont.convertOneLine(
               "Hello " + ((args.length>0)?args[0]:"jbang")));  ;;
    }
}
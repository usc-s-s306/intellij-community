// "Replace with allMatch()" "true"

public class Main {
  boolean find(String[][] data) {
    for(String[] arr : da<caret>ta) {
      for(String str : arr) {
        if(!str.startsWith("xyz")) {
          // Comment
          return false;
        }
      }
    }
    return true;
  }
}

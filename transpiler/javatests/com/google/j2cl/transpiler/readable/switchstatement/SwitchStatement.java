package com.google.j2cl.transpiler.readable.switchstatement;

public class SwitchStatement {
  public void main() {
    // String switch.
    switch ("one") {
      case "one":
      case "two":
        break;
      default:
        return;
    }

    // Char switch.
    switch ('1') {
      case '1':
      case '2':
        break;
      default:
        return;
    }

    // Int switch.
    switch (1) {
      case 1:
      case 2:
        break;
      default:
        return;
    }

    // Enum switch.
    switch (Numbers.ONE) {
      case ONE:
      case TWO:
        break;
      default:
        return;
    }
  }
}

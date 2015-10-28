/**
 * As we are not running the transpiler, this file is hand rolled as a mock.
 * It mocks the Main constructor and adds the $clinit static method which
 * sets the car clinit_called to true so that we can verify it was called in
 * the tests.
 */

var clinit_called = false;

var Main = function() {};
Main.$clinit = function() {
  clinit_called = true;
}

/* Protocol:
 * OOOOXXYY
 * 
 * O: Opcode
 *    0 = client to server to set field
 *    1 = server to client to set "X"
 *    2 = server to client to set "O"
 *    3 = server to client to indicate win
 *    4 = server to client to indicate loss
 *    5 = server to client to indicate tie
 * X: X coord
 * Y: Y coord
 */

public class Protocol {	
	public static int opcode(int input) {
		return (input & 0xF0) >> 4;
	}
	
	public static int x(int input) {
		return (input & 0xC) >> 2;
	}
	
	public static int y(int input) {
		return input & 3;
	}
}

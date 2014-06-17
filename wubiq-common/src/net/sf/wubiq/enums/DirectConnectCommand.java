/**
 * 
 */
package net.sf.wubiq.enums;

/**
 * Indicates the subset of commands for direct connection.
 * @author Federico Alcantara
 *
 */
public enum DirectConnectCommand {
	START, POLL, DATA, READ_REMOTE, POLL_REMOTE_DATA, EXCEPTION, RUNTIME_EXCEPTION;
}
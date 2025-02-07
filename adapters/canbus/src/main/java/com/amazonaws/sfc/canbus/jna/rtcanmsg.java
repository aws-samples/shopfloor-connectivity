package com.amazonaws.sfc.canbus.jna;

import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class rtcanmsg extends Structure {
	/** C type : __u8 */
	public byte can_family;
	/** C type : __u8 */
	public byte gwtype;
	/** C type : __u16 */
	public short flags;
	public rtcanmsg() {
		super();
	}
	protected List<String> getFieldOrder() {
		return Arrays.asList("can_family", "gwtype", "flags");
	}
	/**
	 * @param can_family C type : __u8<br>
	 * @param gwtype C type : __u8<br>
	 * @param flags C type : __u16
	 */
	public rtcanmsg(byte can_family, byte gwtype, short flags) {
		super();
		this.can_family = can_family;
		this.gwtype = gwtype;
		this.flags = flags;
	}
	public static class ByReference extends rtcanmsg implements Structure.ByReference {
		
	};
	public static class ByValue extends rtcanmsg implements Structure.ByValue {
		
	};
}

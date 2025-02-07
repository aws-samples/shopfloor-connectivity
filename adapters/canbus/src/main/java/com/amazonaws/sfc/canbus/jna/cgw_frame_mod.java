package com.amazonaws.sfc.canbus.jna;

import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class cgw_frame_mod extends Structure {
	/** C type : can_frame */
	public can_frame cf;
	/** C type : __u8 */
	public byte modtype;
	public cgw_frame_mod() {
		super();
	}
	protected List<String> getFieldOrder() {
		return Arrays.asList("cf", "modtype");
	}
	/**
	 * @param cf C type : can_frame<br>
	 * @param modtype C type : __u8
	 */
	public cgw_frame_mod(can_frame cf, byte modtype) {
		super();
		this.cf = cf;
		this.modtype = modtype;
	}
	public static class ByReference extends cgw_frame_mod implements Structure.ByReference {
		
	};
	public static class ByValue extends cgw_frame_mod implements Structure.ByValue {
		
	};
}

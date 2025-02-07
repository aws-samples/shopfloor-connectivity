package com.amazonaws.sfc.canbus.jna;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class if_nameindex extends Structure {
	/** 1, 2, ... */
	public int if_index;
	/**
	 * null terminated name: "eth0", ...<br>
	 * C type : char*
	 */
	public Pointer if_name;
	public if_nameindex() {
		super();
	}
	protected List<String> getFieldOrder() {
		return Arrays.asList("if_index", "if_name");
	}
	/**
	 * @param if_index 1, 2, ...<br>
	 * @param if_name null terminated name: "eth0", ...<br>
	 * C type : char*
	 */
	public if_nameindex(int if_index, Pointer if_name) {
		super();
		this.if_index = if_index;
		this.if_name = if_name;
	}
	public static class ByReference extends if_nameindex implements Structure.ByReference {
		
	};
	public static class ByValue extends if_nameindex implements Structure.ByValue {
		
	};
}

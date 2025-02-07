package com.amazonaws.sfc.canbus.jna;

import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class cgw_csum_xor extends Structure {
	public byte from_idx;
	public byte to_idx;
	public byte result_idx;
	/** C type : __u8 */
	public byte init_xor_val;
	public cgw_csum_xor() {
		super();
	}
	protected List<String> getFieldOrder() {
		return Arrays.asList("from_idx", "to_idx", "result_idx", "init_xor_val");
	}
	/** @param init_xor_val C type : __u8 */
	public cgw_csum_xor(byte from_idx, byte to_idx, byte result_idx, byte init_xor_val) {
		super();
		this.from_idx = from_idx;
		this.to_idx = to_idx;
		this.result_idx = result_idx;
		this.init_xor_val = init_xor_val;
	}
	public static class ByReference extends cgw_csum_xor implements Structure.ByReference {
		
	};
	public static class ByValue extends cgw_csum_xor implements Structure.ByValue {
		
	};
}

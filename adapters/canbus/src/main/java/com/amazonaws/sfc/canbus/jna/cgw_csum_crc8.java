package com.amazonaws.sfc.canbus.jna;

import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class cgw_csum_crc8 extends Structure {
	public byte from_idx;
	public byte to_idx;
	public byte result_idx;
	/** C type : __u8 */
	public byte init_crc_val;
	/** C type : __u8 */
	public byte final_xor_val;
	/** C type : __u8[256] */
	public byte[] crctab = new byte[256];
	/** C type : __u8 */
	public byte profile;
	/** C type : __u8[20] */
	public byte[] profile_data = new byte[20];
	public cgw_csum_crc8() {
		super();
	}
	protected List<String> getFieldOrder() {
		return Arrays.asList("from_idx", "to_idx", "result_idx", "init_crc_val", "final_xor_val", "crctab", "profile", "profile_data");
	}
	/**
	 * @param init_crc_val C type : __u8<br>
	 * @param final_xor_val C type : __u8<br>
	 * @param crctab C type : __u8[256]<br>
	 * @param profile C type : __u8<br>
	 * @param profile_data C type : __u8[20]
	 */
	public cgw_csum_crc8(byte from_idx, byte to_idx, byte result_idx, byte init_crc_val, byte final_xor_val, byte crctab[], byte profile, byte profile_data[]) {
		super();
		this.from_idx = from_idx;
		this.to_idx = to_idx;
		this.result_idx = result_idx;
		this.init_crc_val = init_crc_val;
		this.final_xor_val = final_xor_val;
		if ((crctab.length != this.crctab.length)) 
			throw new IllegalArgumentException("Wrong array size !");
		this.crctab = crctab;
		this.profile = profile;
		if ((profile_data.length != this.profile_data.length)) 
			throw new IllegalArgumentException("Wrong array size !");
		this.profile_data = profile_data;
	}
	public static class ByReference extends cgw_csum_crc8 implements Structure.ByReference {
		
	};
	public static class ByValue extends cgw_csum_crc8 implements Structure.ByValue {
		
	};
}

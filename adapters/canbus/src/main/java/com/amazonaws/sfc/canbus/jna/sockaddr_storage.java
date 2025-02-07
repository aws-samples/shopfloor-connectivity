package com.amazonaws.sfc.canbus.jna;

import com.sun.jna.NativeLong;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public abstract class sockaddr_storage extends Structure {
	/**
	 * Address family, etc.<br>
	 * C type : sa_family_t
	 */
	public short ss_family;
	/** Force desired alignment. */
	public NativeLong __ss_align;
	/** Conversion Error : sizeof(unsigned long) */
	public sockaddr_storage() {
		super();
	}
	protected List<String> getFieldOrder() {
		return Arrays.asList("ss_family", "__ss_align");
	}
	/**
	 * @param ss_family Address family, etc.<br>
	 * C type : sa_family_t<br>
	 * @param __ss_align Force desired alignment.
	 */
	public sockaddr_storage(short ss_family, NativeLong __ss_align) {
		super();
		this.ss_family = ss_family;
		this.__ss_align = __ss_align;
	}
	public static abstract class ByReference extends sockaddr_storage implements Structure.ByReference {
		
	};
	public static abstract class ByValue extends sockaddr_storage implements Structure.ByValue {
		
	};
}

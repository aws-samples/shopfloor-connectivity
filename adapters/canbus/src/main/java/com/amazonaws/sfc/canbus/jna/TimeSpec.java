package com.amazonaws.sfc.canbus.jna;

import com.sun.jna.NativeLong;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class TimeSpec extends Structure {
	/**
	 * seconds<br>
	 * C type : __kernel_time_t
	 */
	public NativeLong tv_sec;
	/** nanoseconds */
	public NativeLong tv_nsec;
	public TimeSpec() {
		super();
	}
	protected List<String> getFieldOrder() {
		return Arrays.asList("tv_sec", "tv_nsec");
	}
	/**
	 * @param tv_sec seconds<br>
	 * C type : __kernel_time_t<br>
	 * @param tv_nsec nanoseconds
	 */
	public TimeSpec(NativeLong tv_sec, NativeLong tv_nsec) {
		super();
		this.tv_sec = tv_sec;
		this.tv_nsec = tv_nsec;
	}
	public static class ByReference extends TimeSpec implements Structure.ByReference {
		
	};
	public static class ByValue extends TimeSpec implements Structure.ByValue {
		
	};
}

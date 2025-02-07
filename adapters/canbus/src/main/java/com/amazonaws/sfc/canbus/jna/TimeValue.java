package com.amazonaws.sfc.canbus.jna;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class TimeValue extends Structure {
	/**
	 * seconds<br>
	 * C type : __kernel_time_t
	 */
	public NativeLong tv_sec;
	/**
	 * microseconds<br>
	 * C type : __kernel_suseconds_t
	 */
	public NativeLong tv_usec;
	public TimeValue() {
		super();
	}
	public TimeValue(Pointer ptr) {
		super(ptr);
	}
	protected List<String > getFieldOrder() {
		return Arrays.asList("tv_sec", "tv_usec");
	}
	/**
	 * @param tv_sec seconds<br>
	 * C type : __kernel_time_t<br>
	 * @param tv_usec microseconds<br>
	 * C type : __kernel_suseconds_t
	 */
	public TimeValue(long tv_sec, long tv_usec) {
		super();
		this.tv_sec = new NativeLong(tv_sec);
		this.tv_usec = new NativeLong(tv_usec);
	}
	/**
	 * @param tv_sec seconds<br>
	 * C type : __kernel_time_t<br>
	 * @param tv_usec microseconds<br>
	 * C type : __kernel_suseconds_t
	 */
	public TimeValue(NativeLong tv_sec, NativeLong tv_usec) {
		super();
		this.tv_sec = tv_sec;
		this.tv_usec = tv_usec;
	}
	public static class ByReference extends TimeValue implements Structure.ByReference {

	};
	public static class ByValue extends TimeValue implements Structure.ByValue {

	};
}

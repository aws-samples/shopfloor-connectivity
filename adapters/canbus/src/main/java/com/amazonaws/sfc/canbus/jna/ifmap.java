package com.amazonaws.sfc.canbus.jna;

import com.sun.jna.NativeLong;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class ifmap extends Structure {
	public NativeLong mem_start;
	public NativeLong mem_end;
	public short base_addr;
	public byte irq;
	public byte dma;
	public byte port;
	public ifmap() {
		super();
	}
	protected List<String> getFieldOrder() {
		return Arrays.asList("mem_start", "mem_end", "base_addr", "irq", "dma", "port");
	}
	public ifmap(NativeLong mem_start, NativeLong mem_end, short base_addr, byte irq, byte dma, byte port) {
		super();
		this.mem_start = mem_start;
		this.mem_end = mem_end;
		this.base_addr = base_addr;
		this.irq = irq;
		this.dma = dma;
		this.port = port;
	}
	public static class ByReference extends ifmap implements Structure.ByReference {
		
	};
	public static class ByValue extends ifmap implements Structure.ByValue {
		
	};
}

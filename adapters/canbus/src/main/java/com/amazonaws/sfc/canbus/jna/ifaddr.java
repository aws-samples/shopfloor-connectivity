package com.amazonaws.sfc.canbus.jna;

import com.amazonaws.sfc.canbus.jna.CLibrary.iface;
import com.sun.jna.Structure;
import com.sun.jna.Union;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class ifaddr extends Structure {
	/**
	 * Address of interface.<br>
	 * C type : sockaddr
	 */
	public sockaddr ifa_addr;
	/** C type : ifa_ifu_union */
	public ifa_ifu_union ifa_ifu;
	/**
	 * Back-pointer to interface.<br>
	 * C type : iface*
	 */
	public iface ifa_ifp;
	/**
	 * Next address for interface.<br>
	 * C type : ifaddr*
	 */
	public ByReference ifa_next;
	/** <i>native declaration : /usr/include/net/if.h:90</i> */
	public static class ifa_ifu_union extends Union {
		/** C type : sockaddr */
		public sockaddr ifu_broadaddr;
		/** C type : sockaddr */
		public sockaddr ifu_dstaddr;
		public ifa_ifu_union() {
			super();
		}
		/** @param ifu_broadaddr_or_ifu_dstaddr C type : sockaddr, or C type : sockaddr */
		public ifa_ifu_union(sockaddr ifu_broadaddr_or_ifu_dstaddr) {
			super();
			this.ifu_dstaddr = this.ifu_broadaddr = ifu_broadaddr_or_ifu_dstaddr;
			setType(sockaddr.class);
		}
		public static class ByReference extends ifa_ifu_union implements Structure.ByReference {
			
		};
		public static class ByValue extends ifa_ifu_union implements Structure.ByValue {
			
		};
	};
	public ifaddr() {
		super();
	}
	protected List<String> getFieldOrder() {
		return Arrays.asList("ifa_addr", "ifa_ifu", "ifa_ifp", "ifa_next");
	}
	/**
	 * @param ifa_addr Address of interface.<br>
	 * C type : sockaddr<br>
	 * @param ifa_ifu C type : ifa_ifu_union<br>
	 * @param ifa_ifp Back-pointer to interface.<br>
	 * C type : iface*<br>
	 * @param ifa_next Next address for interface.<br>
	 * C type : ifaddr*
	 */
	public ifaddr(sockaddr ifa_addr, ifa_ifu_union ifa_ifu, iface ifa_ifp, ByReference ifa_next) {
		super();
		this.ifa_addr = ifa_addr;
		this.ifa_ifu = ifa_ifu;
		this.ifa_ifp = ifa_ifp;
		this.ifa_next = ifa_next;
	}
	public static class ByReference extends ifaddr implements Structure.ByReference {
		
	};
	public static class ByValue extends ifaddr implements Structure.ByValue {
		
	};
}

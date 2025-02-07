package com.amazonaws.sfc.canbus.jna;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Union;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class ifreq extends Structure {
	/** C type : ifr_ifrn_union */
	public ifr_ifrn_union ifr_ifrn;
	/** C type : ifr_ifru_union */
	public ifr_ifru_union ifr_ifru;
	/** <i>native declaration : /usr/include/net/if.h:129</i> */
	public static class ifr_ifrn_union extends Union {
		/**
		 * Interface name, e.g. "en0".<br>
		 * C type : char[16]
		 */
		public byte[] ifrn_name = new byte[16];
		public ifr_ifrn_union() {
			super();
		}
		/**
		 * @param ifrn_name Interface name, e.g. "en0".<br>
		 * C type : char[16]
		 */
		public ifr_ifrn_union(byte ifrn_name[]) {
			super();
			if ((ifrn_name.length != this.ifrn_name.length)) 
				throw new IllegalArgumentException("Wrong array size !");
			this.ifrn_name = ifrn_name;
			setType(byte[].class);
		}
		public static class ByReference extends ifr_ifrn_union implements Structure.ByReference {
			
		};
		public static class ByValue extends ifr_ifrn_union implements Structure.ByValue {
			
		};
	};
	/** <i>native declaration : /usr/include/net/if.h:134</i> */
	public static class ifr_ifru_union extends Union {
		/** C type : sockaddr */
		public sockaddr ifru_addr;
		/** C type : sockaddr */
		public sockaddr ifru_dstaddr;
		/** C type : sockaddr */
		public sockaddr ifru_broadaddr;
		/** C type : sockaddr */
		public sockaddr ifru_netmask;
		/** C type : sockaddr */
		public sockaddr ifru_hwaddr;
		public short ifru_flags;
		public int ifru_ivalue;
		public int ifru_mtu;
		/** C type : ifmap */
		public ifmap ifru_map;
		/**
		 * Just fits the size<br>
		 * C type : char[16]
		 */
		public byte[] ifru_slave = new byte[16];
		/** C type : char[16] */
		public byte[] ifru_newname = new byte[16];
		/** C type : __caddr_t */
		public Pointer ifru_data;
		public ifr_ifru_union() {
			super();
		}
		public ifr_ifru_union(short ifru_flags) {
			super();
			this.ifru_flags = ifru_flags;
			setType(Short.TYPE);
		}
		/** @param ifru_addr_or_ifru_dstaddr_or_ifru_broadaddr_or_ifru_netmask_or_ifru_hwaddr C type : sockaddr, or C type : sockaddr, or C type : sockaddr, or C type : sockaddr, or C type : sockaddr */
		public ifr_ifru_union(sockaddr ifru_addr_or_ifru_dstaddr_or_ifru_broadaddr_or_ifru_netmask_or_ifru_hwaddr) {
			super();
			this.ifru_hwaddr = this.ifru_netmask = this.ifru_broadaddr = this.ifru_dstaddr = this.ifru_addr = ifru_addr_or_ifru_dstaddr_or_ifru_broadaddr_or_ifru_netmask_or_ifru_hwaddr;
			setType(sockaddr.class);
		}
		/** @param ifru_map C type : ifmap */
		public ifr_ifru_union(ifmap ifru_map) {
			super();
			this.ifru_map = ifru_map;
			setType(ifmap.class);
		}
		public ifr_ifru_union(int ifru_ivalue_or_ifru_mtu) {
			super();
			this.ifru_mtu = this.ifru_ivalue = ifru_ivalue_or_ifru_mtu;
			setType(Integer.TYPE);
		}
		/** @param ifru_data C type : __caddr_t */
		public ifr_ifru_union(Pointer ifru_data) {
			super();
			this.ifru_data = ifru_data;
			setType(Pointer.class);
		}
		/**
		 * @param ifru_slave_or_ifru_newname Just fits the size<br>
		 * C type : char[16], or C type : char[16]
		 */
		public ifr_ifru_union(byte ifru_slave_or_ifru_newname[]) {
			super();
			if ((ifru_slave_or_ifru_newname.length != this.ifru_newname.length)) 
				throw new IllegalArgumentException("Wrong array size !");
			this.ifru_newname = this.ifru_slave = ifru_slave_or_ifru_newname;
			setType(byte[].class);
		}
		public static class ByReference extends ifr_ifru_union implements Structure.ByReference {
			
		};
		public static class ByValue extends ifr_ifru_union implements Structure.ByValue {
			
		};
	};
	public ifreq() {
		super();
	}
	protected List<String> getFieldOrder() {
		return Arrays.asList("ifr_ifrn", "ifr_ifru");
	}
	/**
	 * @param ifr_ifrn C type : ifr_ifrn_union<br>
	 * @param ifr_ifru C type : ifr_ifru_union
	 */
	public ifreq(ifr_ifrn_union ifr_ifrn, ifr_ifru_union ifr_ifru) {
		super();
		this.ifr_ifrn = ifr_ifrn;
		this.ifr_ifru = ifr_ifru;
	}
	public static class ByReference extends ifreq implements Structure.ByReference {
		
	};
	public static class ByValue extends ifreq implements Structure.ByValue {
		
	};
}

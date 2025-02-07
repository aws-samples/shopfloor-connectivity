package com.amazonaws.sfc.canbus.jna;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Union;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class ifconf extends Structure {
	/** Size of buffer. */
	public int ifc_len;
	/** C type : ifc_ifcu_union */
	public ifc_ifcu_union ifc_ifcu;
	/** <i>native declaration : /usr/include/net/if.h:178</i> */
	public static class ifc_ifcu_union extends Union {
		/** C type : __caddr_t */
		public Pointer ifcu_buf;
		/** C type : ifreq* */
		public ifreq.ByReference ifcu_req;
		public ifc_ifcu_union() {
			super();
		}
		/** @param ifcu_req C type : ifreq* */
		public ifc_ifcu_union(ifreq.ByReference ifcu_req) {
			super();
			this.ifcu_req = ifcu_req;
			setType(ifreq.ByReference.class);
		}
		/** @param ifcu_buf C type : __caddr_t */
		public ifc_ifcu_union(Pointer ifcu_buf) {
			super();
			this.ifcu_buf = ifcu_buf;
			setType(Pointer.class);
		}
		public static class ByReference extends ifc_ifcu_union implements Structure.ByReference {
			
		};
		public static class ByValue extends ifc_ifcu_union implements Structure.ByValue {
			
		};
	};
	public ifconf() {
		super();
	}
	protected List<String> getFieldOrder() {
		return Arrays.asList("ifc_len", "ifc_ifcu");
	}
	/**
	 * @param ifc_len Size of buffer.<br>
	 * @param ifc_ifcu C type : ifc_ifcu_union
	 */
	public ifconf(int ifc_len, ifc_ifcu_union ifc_ifcu) {
		super();
		this.ifc_len = ifc_len;
		this.ifc_ifcu = ifc_ifcu;
	}
	public static class ByReference extends ifconf implements Structure.ByReference {
		
	};
	public static class ByValue extends ifconf implements Structure.ByValue {
		
	};
}

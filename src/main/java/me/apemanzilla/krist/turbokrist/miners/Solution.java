package me.apemanzilla.krist.turbokrist.miners;

import me.apemanzilla.krist.state.NodeState;
import me.apemanzilla.krist.turbokrist.MinerOptions;
import me.lignum.jkrist.Address;

/**
 * Represents a solution that can be submitted to the Krist syncnode.
 * 
 * @author apemanzilla
 *
 */
public class Solution {
	private final String block;
	private final String address;
	private final String nonce;

	// TODO: Implement checking of solution validity in constructors

	public Solution(String address, String block, String nonce) {
		this.block = block;
		this.address = address;
		this.nonce = nonce;
	}

	public String getBlock() {
		return block;
	}

	public String getAddress() {
		return address;
	}

	public String getNonce() {
		return nonce;
	}

	@Override
	public String toString() {
		return address + block + nonce;
	}

}

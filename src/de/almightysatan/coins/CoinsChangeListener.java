package de.almightysatan.coins;

import java.util.UUID;

public interface CoinsChangeListener {

	public void onCoinsChange(UUID uuid, int oldBalance, int newBalance);
}

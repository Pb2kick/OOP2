package ambot.level;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import ambot.save.SaveData;

class CampaignTest {
    private final Campaign campaign = Campaign.standard();
    private final World w1 = campaign.world(1);

    static SaveData done(String... ids) {
        return new SaveData(Set.of(ids), 7, 8, false);
    }

    @Test
    void standardHasThreeWorldsWithWorldOnePlayable() {
        assertEquals(3, campaign.worlds().size());
        assertEquals(4, w1.levels().size());
        assertTrue(w1.available());
        assertFalse(campaign.world(2).available());
        assertFalse(campaign.world(3).available());
        assertEquals("First Blood", w1.levels().get(3).title());
    }

    @Test
    void onlyFirstLevelUnlockedOnANewGame() {
        SaveData fresh = SaveData.defaults();
        assertTrue(campaign.isUnlocked(w1, 0, fresh));
        for (int i = 1; i < w1.levels().size(); i++) assertFalse(campaign.isUnlocked(w1, i, fresh));
    }

    @Test
    void completingOneLevelUnlocksOnlyTheNext() {
        SaveData d = done("w1-1");
        assertTrue(campaign.isUnlocked(w1, 1, d));
        assertFalse(campaign.isUnlocked(w1, 2, d));
    }

    @Test
    void completedLevelsStayPlayable() {
        SaveData d = done("w1-1", "w1-2");
        assertTrue(campaign.isUnlocked(w1, 0, d));
        assertTrue(campaign.isUnlocked(w1, 1, d));
    }

    @Test
    void unavailableWorldNeverUnlocks() {
        SaveData all = done("w1-1", "w1-2", "w1-3", "w1-4");
        assertFalse(campaign.isUnlocked(campaign.world(2), 0, all));
    }

    @Test
    void nextUnfinishedFollowsProgress() {
        assertArrayEquals(new int[] { 1, 0 }, campaign.nextUnfinished(SaveData.defaults()).orElseThrow());
        assertArrayEquals(new int[] { 1, 1 }, campaign.nextUnfinished(done("w1-1")).orElseThrow());
        assertTrue(campaign.nextUnfinished(done("w1-1", "w1-2", "w1-3", "w1-4")).isEmpty());
    }

    @Test
    void catalogCarriesIds() {
        LevelCatalog c = campaign.catalog(w1);
        assertEquals(4, c.size());
        assertEquals("w1-3", c.id(2));
        assertEquals(1, campaign.worldOf(c).orElseThrow().number());
    }

    @Test
    void ofCatalogIds() {
        Level a = TestLevels.ascii(".P..E.", "######");
        LevelCatalog c = LevelCatalog.of(a, a);
        assertEquals("level-2", c.id(1));
        assertTrue(campaign.worldOf(c).isEmpty());
        assertEquals("p1", LevelCatalog.placeholder().id(0));
    }
}

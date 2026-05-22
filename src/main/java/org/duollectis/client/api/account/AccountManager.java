package org.duollectis.client.api.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Менеджер оффлайн-аккаунтов: хранит список ников, управляет активным аккаунтом,
 * сохраняет и загружает данные из {@code duollectis/accounts.json}.
 * Именованные группы хранятся в {@code duollectis/groups.json} в пользовательском порядке.
 */
@Slf4j
public class AccountManager {

	@Getter
	private static AccountManager instance;

	private final List<Account> accountsSet = new ArrayList<>();

	/** Группы, созданные вручную — порядок определяется пользователем. */
	private final List<String> namedGroups = new ArrayList<>();

	@Getter
	private Account activeAccount;

	private static final Gson ACCOUNT_GSON = new GsonBuilder()
			.setPrettyPrinting()
			.excludeFieldsWithoutExposeAnnotation()
			.create();

	private static final Type ACCOUNT_LIST_TYPE = new TypeToken<List<Account>>() {}.getType();
	private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() {}.getType();

	public AccountManager() {
		instance = this;
		load();
		loadGroups();
	}

	public List<Account> getAccounts() {
		return new ArrayList<>(accountsSet);
	}

	public List<String> getNamedGroups() {
		return List.copyOf(namedGroups);
	}

	public void addNamedGroup(String name) {
		namedGroups.add(name);
		saveGroups();
	}

	public void removeNamedGroup(String name) {
		namedGroups.remove(name);
		saveGroups();
	}

	public void renameNamedGroup(String oldName, String newName) {
		if (namedGroups.remove(oldName) == false) {
			return;
		}

		namedGroups.add(newName);
		saveGroups();
	}

	/** Перемещает группу на одну позицию вверх в списке именованных групп. */
	public void moveGroupUp(String name) {
		List<String> list = new ArrayList<>(namedGroups);
		int idx = list.indexOf(name);

		if (idx <= 0) {
			return;
		}

		list.remove(idx);
		list.add(idx - 1, name);
		namedGroups.clear();
		namedGroups.addAll(list);
		saveGroups();
	}

	/** Перемещает группу на одну позицию вниз в списке именованных групп. */
	public void moveGroupDown(String name) {
		List<String> list = new ArrayList<>(namedGroups);
		int idx = list.indexOf(name);

		if (idx < 0 || idx >= list.size() - 1) {
			return;
		}

		list.remove(idx);
		list.add(idx + 1, name);
		namedGroups.clear();
		namedGroups.addAll(list);
		saveGroups();
	}

	public void addAccount(Account account) {
		accountsSet.add(account);
		save();
	}

	/**
	 * Вставляет точную копию аккаунта (то же имя, та же группа) сразу после оригинала.
	 */
	public Account duplicateAccount(Account original) {
		Account duplicate = new Account(original.getUsername());
		duplicate.setGroup(original.getGroup());

		int index = accountsSet.indexOf(original);
		int insertAt = index >= 0 ? index + 1 : accountsSet.size();
		accountsSet.add(insertAt, duplicate);
		save();

		return duplicate;
	}

	public void removeAccount(Account account) {
		if (activeAccount == account) {
			activeAccount = null;
		}

		accountsSet.removeIf(a -> a == account);
		save();
	}

	/** Перемещает аккаунт на одну позицию вверх внутри его группы. */
	public void moveAccountUp(Account account) {
		List<Account> list = new ArrayList<>(accountsSet);
		String group = account.getGroup();
		List<Integer> groupIndices = new ArrayList<>();

		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).getGroup().equals(group)) {
				groupIndices.add(i);
			}
		}

		int posInGroup = groupIndices.indexOf(list.indexOf(account));

		if (posInGroup <= 0) {
			return;
		}

		int globalIdx = list.indexOf(account);
		int prevGlobalIdx = groupIndices.get(posInGroup - 1);

		list.remove(globalIdx);
		list.add(prevGlobalIdx, account);

		accountsSet.clear();
		accountsSet.addAll(list);
		save();
	}

	/** Перемещает аккаунт на одну позицию вниз внутри его группы. */
	public void moveAccountDown(Account account) {
		List<Account> list = new ArrayList<>(accountsSet);
		String group = account.getGroup();
		List<Integer> groupIndices = new ArrayList<>();

		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).getGroup().equals(group)) {
				groupIndices.add(i);
			}
		}

		int posInGroup = groupIndices.indexOf(list.indexOf(account));

		if (posInGroup < 0 || posInGroup >= groupIndices.size() - 1) {
			return;
		}

		int globalIdx = list.indexOf(account);
		int nextGlobalIdx = groupIndices.get(posInGroup + 1);

		list.remove(globalIdx);
		list.add(nextGlobalIdx, account);

		accountsSet.clear();
		accountsSet.addAll(list);
		save();
	}

	/** Сортирует аккаунты указанной группы по никнейму без учёта регистра. */
	public void sortGroupAccounts(String group) {
		List<Account> list = new ArrayList<>(accountsSet);
		List<Account> groupMembers = list.stream()
				.filter(a -> a.getGroup().equals(group))
				.sorted(Comparator.comparing(a -> a.getUsername().toLowerCase()))
				.toList();

		list.removeIf(a -> a.getGroup().equals(group));

		// Находим позицию первого аккаунта этой группы для вставки
		int insertAt = list.size();

		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).getGroup().equals(group)) {
				insertAt = i;
				break;
			}
		}

		list.addAll(insertAt, groupMembers);

		accountsSet.clear();
		accountsSet.addAll(list);
		save();
	}

	public void setActiveAccount(Account account) {
		activeAccount = account;
	}

	public boolean contains(String username) {
		return accountsSet.stream()
				.anyMatch(a -> a.getUsername().equalsIgnoreCase(username));
	}

	public void save() {
		File file = accountFile();
		file.getParentFile().mkdirs();

		try (FileWriter writer = new FileWriter(file)) {
			ACCOUNT_GSON.toJson(new ArrayList<>(accountsSet), ACCOUNT_LIST_TYPE, writer);
		} catch (IOException e) {
			log.error("Не удалось сохранить список аккаунтов", e);
		}
	}

	public void saveGroups() {
		File file = groupsFile();
		file.getParentFile().mkdirs();

		try (FileWriter writer = new FileWriter(file)) {
			ACCOUNT_GSON.toJson(new ArrayList<>(namedGroups), STRING_LIST_TYPE, writer);
		} catch (IOException e) {
			log.error("Не удалось сохранить список групп", e);
		}
	}

	private void load() {
		File file = accountFile();

		if (file.exists() == false) {
			return;
		}

		try (FileReader reader = new FileReader(file)) {
			List<Account> loaded = ACCOUNT_GSON.fromJson(reader, ACCOUNT_LIST_TYPE);

			if (loaded == null) {
				return;
			}

			accountsSet.addAll(loaded);
		} catch (IOException e) {
			log.error("Не удалось загрузить список аккаунтов", e);
		}
	}

	private void loadGroups() {
		File file = groupsFile();

		if (file.exists() == false) {
			return;
		}

		try (FileReader reader = new FileReader(file)) {
			List<String> loaded = ACCOUNT_GSON.fromJson(reader, STRING_LIST_TYPE);

			if (loaded == null) {
				return;
			}

			namedGroups.addAll(loaded);
		} catch (IOException e) {
			log.error("Не удалось загрузить список групп", e);
		}
	}

	private static File accountFile() {
		return new File(FabricLoader.getInstance().getGameDir().toFile(), "duollectis/accounts.json");
	}

	private static File groupsFile() {
		return new File(FabricLoader.getInstance().getGameDir().toFile(), "duollectis/groups.json");
	}
}

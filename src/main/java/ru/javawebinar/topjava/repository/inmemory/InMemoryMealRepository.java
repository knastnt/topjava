package ru.javawebinar.topjava.repository.inmemory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.javawebinar.topjava.model.Meal;
import ru.javawebinar.topjava.repository.MealRepository;
import ru.javawebinar.topjava.util.MealsUtil;
import ru.javawebinar.topjava.util.exception.PermissionException;
import ru.javawebinar.topjava.web.SecurityUtil;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class InMemoryMealRepository implements MealRepository {
    private static final Logger log = LoggerFactory.getLogger(InMemoryMealRepository.class);

    private Map<Integer, Meal> repository = new ConcurrentHashMap<>();
    private AtomicInteger counter = new AtomicInteger(0);

    {
        MealsUtil.MEALS.forEach(this::save);
    }

    @Override
    public Meal save(Meal meal) {
        log.info("save {}", meal);

        synchronized (meal) {
            if (!SecurityUtil.isMealBelongsToAuthUser(meal))
                throw new PermissionException("Saved meal not belongs to you");

            if (meal.isNew()) {
                meal.setId(counter.incrementAndGet());
                repository.put(meal.getId(), meal);
                return meal;
            }
            // handle case: update, but not present in storage
            return repository.computeIfPresent(meal.getId(), (id, oldMeal) -> {
                if (!SecurityUtil.isMealBelongsToAuthUser(oldMeal))
                    throw new PermissionException("Updated meal not belongs to you");
                return meal;
            });
        }
    }

    @Override
    public boolean delete(int id) {
        log.info("delete {}", id);

        Meal meal = repository.get(id);
        synchronized (meal) {
            if (meal == null) return false;

            if (!SecurityUtil.isMealBelongsToAuthUser(meal))
                throw new PermissionException("Deleted meal not belongs to you");

            return repository.remove(id) != null;
        }
    }

    @Override
    public Meal get(int id) {
        log.info("get {}", id);
        Meal meal = repository.get(id);
        if (SecurityUtil.isMealBelongsToAuthUser(meal)){
            return meal;
        }else {
            return null;
        }
    }

    @Override
    public Collection<Meal> getAll() {
        return repository.values().stream()
                .filter(meal -> SecurityUtil.isMealBelongsToAuthUser(meal))
                .sorted((meal1, meal2) -> meal1.getDateTime().compareTo(meal2.getDateTime()))
                .collect(Collectors.toList());
    }

}


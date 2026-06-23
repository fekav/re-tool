package io.fekav.platform.cqrs;

/**
 * I am an incoming port for driving adapters: I accept a Command and forward them
 * to a CommandHandler.
 *
 * @param <R> return type of the dispatch call
 */
public interface CommandBus {
    /**
     * Dispatches a Command to the responsible handler and returns the result.
     *
     * @param command the Command to execute; must not be null (implementations may document different behavior)
     * @param <R>     expected return type
     * @return result of the Command execution
     */
    <R, C extends Command<R>> R dispatch(C command);
}

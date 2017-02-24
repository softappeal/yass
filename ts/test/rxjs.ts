import * as Rx from "@reactivex/rxjs";

/**
 * Calls errorHandler if promise fails instead of Observer.error.
 */
function observableWithErrorHandler<T>(promise: Promise<T>, errorHandler: (err: any) => void): Rx.Observable<T> {
    const subject = new Rx.Subject;
    promise
        .then(value => {
            subject.next(value);
            subject.complete();
        })
        .catch(err => errorHandler(err));
    return subject;
}

function log(...args: any[]): void {
    console.log.apply(console, args);
}

function observer<T>(id: number): Rx.Observer<T> {
    return {
        next: function (value: T): void {
            log("next", id, ":", value);
        },
        error: function (err: any): void {
            log("error", id, ":", err);
        },
        complete: function (): void {
            log("complete", id);
        }
    };
}

function test<T>(promise: Promise<T>): void {
    const observable = observableWithErrorHandler(promise, err => log("errorHandler :", err));
    observable.subscribe(observer(1));
    observable.subscribe(observer(2));
}
test(Promise.resolve("hello"));
test(Promise.reject("exception"));

/*

 next 1 : hello
 next 2 : hello
 complete 1
 complete 2

 errorHandler : exception

 */
